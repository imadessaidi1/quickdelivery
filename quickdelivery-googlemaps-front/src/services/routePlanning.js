function normalizePoint(point) {
  if (!point) {
    return null;
  }
  const latitude = Number(point.latitude ?? point.lat);
  const longitude = Number(point.longitude ?? point.lng);
  if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
    return null;
  }
  return { lat: latitude, lng: longitude };
}

function getPackageRouteStops(package_) {
  if (!package_ || !Array.isArray(package_.addresses)) {
    return { pickup: null, dropoff: null };
  }
  const pickupAddress = package_.addresses.find((address) => address.type === 'DEPARTURE');
  const dropoffAddress = package_.addresses.find((address) => address.type === 'ARRIVAL');
  return {
    pickup: pickupAddress ? {
      kind: 'pickup',
      packageId: package_.id,
      packageReference: package_.reference,
      addressLabel: pickupAddress.addressAuto || `${pickupAddress.line1 || ''} ${pickupAddress.zipCode || ''} ${pickupAddress.town || ''}`.trim(),
      point: normalizePoint(pickupAddress),
    } : null,
    dropoff: dropoffAddress ? {
      kind: 'dropoff',
      packageId: package_.id,
      packageReference: package_.reference,
      addressLabel: dropoffAddress.addressAuto || `${dropoffAddress.line1 || ''} ${dropoffAddress.zipCode || ''} ${dropoffAddress.town || ''}`.trim(),
      point: normalizePoint(dropoffAddress),
    } : null,
  };
}

export function haversineMeters(start, end) {
  if (!start || !end) {
    return Number.MAX_SAFE_INTEGER;
  }
  const toRadians = (value) => (value * Math.PI) / 180;
  const earthRadius = 6371000;
  const deltaLat = toRadians(end.lat - start.lat);
  const deltaLng = toRadians(end.lng - start.lng);
  const a = Math.sin(deltaLat / 2) ** 2
    + Math.cos(toRadians(start.lat)) * Math.cos(toRadians(end.lat)) * Math.sin(deltaLng / 2) ** 2;
  return 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function projectionProgress(point, start, end) {
  if (!point || !start || !end) {
    return 0;
  }
  const midLat = (start.lat + end.lat) / 2;
  const metersPerLat = 111320;
  const metersPerLng = 111320 * Math.cos((midLat * Math.PI) / 180);
  const vx = (end.lng - start.lng) * metersPerLng;
  const vy = (end.lat - start.lat) * metersPerLat;
  const lengthSquared = (vx * vx) + (vy * vy);
  if (!lengthSquared) {
    return 0;
  }
  const px = (point.lng - start.lng) * metersPerLng;
  const py = (point.lat - start.lat) * metersPerLat;
  return ((px * vx) + (py * vy)) / lengthSquared;
}

function normalizeRouteProgress(point, start, end) {
  const rawProgress = projectionProgress(point, start, end);
  if (!Number.isFinite(rawProgress)) {
    return 0;
  }
  return Math.max(0, Math.min(1, rawProgress));
}

function orderStopsByNearestNeighbor(initialPoint, stops, finalBiasPoint = null) {
  const pendingStops = Array.isArray(stops) ? [...stops] : [];
  const orderedStops = [];
  let currentPoint = initialPoint;
  while (pendingStops.length) {
    let bestIndex = 0;
    let bestScore = Number.MAX_SAFE_INTEGER;
    pendingStops.forEach((stop, index) => {
      const distanceFromCurrent = haversineMeters(currentPoint, { lat: stop.lat, lng: stop.lng });
      const distanceToFinalBias = finalBiasPoint
        ? haversineMeters({ lat: stop.lat, lng: stop.lng }, finalBiasPoint)
        : 0;
      const score = distanceFromCurrent + (distanceToFinalBias * 0.1);
      if (score < bestScore) {
        bestScore = score;
        bestIndex = index;
      }
    });
    const [selectedStop] = pendingStops.splice(bestIndex, 1);
    orderedStops.push(selectedStop);
    currentPoint = { lat: selectedStop.lat, lng: selectedStop.lng };
  }
  return orderedStops;
}

function computePathDistance(start, stops, end) {
  const nodes = [
    start,
    ...(Array.isArray(stops) ? stops.map((stop) => ({ lat: stop.lat, lng: stop.lng })) : []),
    end,
  ].filter(Boolean);
  let totalMeters = 0;
  for (let index = 0; index < nodes.length - 1; index += 1) {
    totalMeters += haversineMeters(nodes[index], nodes[index + 1]);
  }
  return totalMeters;
}

function optimizeStopsWithTwoOpt(start, stops, end, maxPasses = 2) {
  if (!Array.isArray(stops) || stops.length < 4) {
    return Array.isArray(stops) ? stops : [];
  }

  const optimized = [...stops];
  let bestDistance = computePathDistance(start, optimized, end);
  let pass = 0;
  let improved = true;

  while (improved && pass < maxPasses) {
    improved = false;
    pass += 1;
    for (let i = 0; i < optimized.length - 2; i += 1) {
      for (let j = i + 1; j < optimized.length - 1; j += 1) {
        const candidate = [
          ...optimized.slice(0, i),
          ...optimized.slice(i, j + 1).reverse(),
          ...optimized.slice(j + 1),
        ];
        const candidateDistance = computePathDistance(start, candidate, end);
        if (candidateDistance + 1 < bestDistance) {
          optimized.splice(0, optimized.length, ...candidate);
          bestDistance = candidateDistance;
          improved = true;
        }
      }
    }
  }

  return optimized;
}

function computePlanMetrics(start, end, stops, packages) {
  const totalDistanceMeters = computePathDistance(start, stops, end);

  const directDistanceMeters = haversineMeters(start, end);
  const detourMeters = Math.max(0, totalDistanceMeters - directDistanceMeters);
  const detourRatio = directDistanceMeters > 0 ? totalDistanceMeters / directDistanceMeters : 1;
  const totalDisplayedAmount = (Array.isArray(packages) ? packages : []).reduce(
    (total, pkg) => total + Number(pkg?.routeDisplayedAmount || 0),
    0,
  );
  const estimatedDurationMinutes = totalDistanceMeters / 1000 / 35 * 60;
  const payoutPerKm = totalDistanceMeters > 0 ? totalDisplayedAmount / (totalDistanceMeters / 1000) : 0;
  const payoutPerHour = estimatedDurationMinutes > 0 ? totalDisplayedAmount / (estimatedDurationMinutes / 60) : 0;
  const qualityScore = Math.max(0, Number((payoutPerKm * 10 - Math.max(0, detourRatio - 1) * 12).toFixed(2)));

  return {
    totalDistanceMeters: Number(totalDistanceMeters.toFixed(2)),
    directDistanceMeters: Number(directDistanceMeters.toFixed(2)),
    detourMeters: Number(detourMeters.toFixed(2)),
    detourRatio: Number(detourRatio.toFixed(4)),
    estimatedDurationMinutes: Number(estimatedDurationMinutes.toFixed(1)),
    totalDisplayedAmount: Number(totalDisplayedAmount.toFixed(2)),
    payoutPerKm: Number(payoutPerKm.toFixed(2)),
    payoutPerHour: Number(payoutPerHour.toFixed(2)),
    qualityScore,
  };
}

function finalizeRoutePlan(start, end, packageStops, orderedStops, packages, mode) {
  const annotations = {};
  orderedStops.forEach((stop, index) => {
    const enrichedStop = stop;
    enrichedStop.order = index + 1;
    const key = String(stop.packageId);
    if (!annotations[key]) {
      annotations[key] = { sortOrder: enrichedStop.order };
    }
    if (stop.kind === 'pickup') {
      annotations[key].pickupOrder = enrichedStop.order;
      annotations[key].sortOrder = Math.min(annotations[key].sortOrder, enrichedStop.order);
    } else {
      annotations[key].dropoffOrder = enrichedStop.order;
    }
    annotations[key].stopCount = Number(annotations[key].pickupOrder != null)
      + Number(annotations[key].dropoffOrder != null);
  });

  return {
    mode,
    start,
    end,
    stops: orderedStops,
    packageIds: packageStops.map(({ packageId }) => packageId),
    packageAnnotations: annotations,
    metrics: computePlanMetrics(start, end, orderedStops, packages),
  };
}

export function buildPersonalRoutePlan(packages, startRaw, endRaw) {
  const start = normalizePoint({ latitude: startRaw?.actuallatitude, longitude: startRaw?.actuallongitude });
  const end = normalizePoint({ latitude: endRaw?.latitude, longitude: endRaw?.longitude });
  if (!start || !end || !Array.isArray(packages) || packages.length === 0) {
    return null;
  }

  const packageStops = packages.map((package_) => {
    const { pickup, dropoff } = getPackageRouteStops(package_);
    return pickup && dropoff ? { packageId: package_.id, pickup, dropoff } : null;
  }).filter(Boolean);
  if (!packageStops.length) {
    return null;
  }

  const orderedStops = packageStops
    .flatMap(({ pickup, dropoff }) => {
      const pickupProgress = normalizeRouteProgress(pickup.point, start, end);
      const rawDropoffProgress = normalizeRouteProgress(dropoff.point, start, end);
      const dropoffProgress = Math.max(rawDropoffProgress, pickupProgress + 0.0001);
      return [
        {
          ...pickup,
          lat: pickup.point.lat,
          lng: pickup.point.lng,
          routeProgress: pickupProgress,
          typePriority: 0,
        },
        {
          ...dropoff,
          lat: dropoff.point.lat,
          lng: dropoff.point.lng,
          routeProgress: dropoffProgress,
          typePriority: 1,
        },
      ];
    })
    .sort((left, right) => {
      if (left.routeProgress !== right.routeProgress) {
        return left.routeProgress - right.routeProgress;
      }
      if (left.packageId === right.packageId && left.kind !== right.kind) {
        return left.kind === 'pickup' ? -1 : 1;
      }
      if (left.typePriority !== right.typePriority) {
        return left.typePriority - right.typePriority;
      }
      return Number(left.packageId || 0) - Number(right.packageId || 0);
    });

  return finalizeRoutePlan(start, end, packageStops, orderedStops, packages, 'personalRoute');
}

export function buildDirectRoutePlan(packages, startRaw, endRaw) {
  const start = normalizePoint({ latitude: startRaw?.actuallatitude, longitude: startRaw?.actuallongitude });
  const end = normalizePoint({ latitude: endRaw?.latitude, longitude: endRaw?.longitude });
  if (!start || !end || !Array.isArray(packages) || packages.length === 0) {
    return null;
  }

  const packageStops = packages.map((package_) => {
    const { pickup, dropoff } = getPackageRouteStops(package_);
    return pickup && dropoff ? { packageId: package_.id, pickup, dropoff } : null;
  }).filter(Boolean);
  if (!packageStops.length) {
    return null;
  }

  const pickupStops = packageStops.map(({ pickup }) => ({
    ...pickup,
    lat: pickup.point.lat,
    lng: pickup.point.lng,
  }));
  const dropoffStops = packageStops.map(({ dropoff }) => ({
    ...dropoff,
    lat: dropoff.point.lat,
    lng: dropoff.point.lng,
  }));

  const orderedPickups = optimizeStopsWithTwoOpt(
    start,
    orderStopsByNearestNeighbor(start, pickupStops),
    end,
    2,
  );
  const pickupEnd = orderedPickups.length
    ? { lat: orderedPickups[orderedPickups.length - 1].lat, lng: orderedPickups[orderedPickups.length - 1].lng }
    : start;
  const orderedDropoffs = optimizeStopsWithTwoOpt(
    pickupEnd,
    orderStopsByNearestNeighbor(pickupEnd, dropoffStops, end),
    end,
    2,
  );

  const combined = [...orderedPickups, ...orderedDropoffs];
  const optimized = optimizeStopsWithPrecedence(start, combined, end, packageStops, 2);

  return finalizeRoutePlan(start, end, packageStops, optimized, packages, 'directAddress');
}

function isPrecedenceValid(stops) {
  const pickupSeen = new Set();
  for (const stop of stops) {
    if (stop.kind === 'pickup') {
      pickupSeen.add(stop.packageId);
    } else if (stop.kind === 'dropoff' && !pickupSeen.has(stop.packageId)) {
      return false;
    }
  }
  return true;
}

function optimizeStopsWithPrecedence(start, stops, end, _packageStops, maxPasses = 2) {
  if (!Array.isArray(stops) || stops.length < 4) {
    return Array.isArray(stops) ? stops : [];
  }
  const optimized = [...stops];
  let bestDistance = computePathDistance(start, optimized, end);
  let pass = 0;
  let improved = true;
  while (improved && pass < maxPasses) {
    improved = false;
    pass += 1;
    for (let i = 0; i < optimized.length - 2; i += 1) {
      for (let j = i + 1; j < optimized.length - 1; j += 1) {
        const candidate = [
          ...optimized.slice(0, i),
          ...optimized.slice(i, j + 1).reverse(),
          ...optimized.slice(j + 1),
        ];
        if (!isPrecedenceValid(candidate)) {
          continue;
        }
        const candidateDistance = computePathDistance(start, candidate, end);
        if (candidateDistance + 1 < bestDistance) {
          optimized.splice(0, optimized.length, ...candidate);
          bestDistance = candidateDistance;
          improved = true;
        }
      }
    }
  }
  return optimized;
}

export function decoratePackagesWithRoutePlan(packages, routePlan) {
  if (!routePlan) {
    return Array.isArray(packages) ? packages : [];
  }
  return packages.map((pkg) => {
    const annotation = routePlan.packageAnnotations[String(pkg.id)] || {};
    return {
      ...pkg,
      routePickupOrder: annotation.pickupOrder ?? null,
      routeDropoffOrder: annotation.dropoffOrder ?? null,
      routeStopCount: annotation.stopCount ?? null,
      routeSortOrder: annotation.sortOrder ?? Number.MAX_SAFE_INTEGER,
    };
  });
}
