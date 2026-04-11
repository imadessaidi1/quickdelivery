const pendingControllers = new Set();

export function attachAbortController(config) {
  if (config?.signal) {
    return config;
  }

  const controller = new AbortController();
  const nextConfig = config || {};
  nextConfig.signal = controller.signal;
  nextConfig.__qdAbortController = controller;
  pendingControllers.add(controller);
  return nextConfig;
}

export function releaseAbortController(config) {
  const controller = config?.__qdAbortController;
  if (controller) {
    pendingControllers.delete(controller);
    delete config.__qdAbortController;
  }
}

export function cancelPendingRequests(reason = 'Request cancelled') {
  pendingControllers.forEach((controller) => controller.abort(reason));
  pendingControllers.clear();
}
