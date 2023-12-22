<!-- PackageDetails.vue -->

<template>
  <div class="package-details">
    <h2>Package Details</h2>
    <p>ID: {{ package.id }}</p>
    <p>Version: {{ package.version }}</p>
    <p>Creation Date: {{ package.creationDate }}</p>
    <!-- Ajoutez d'autres champs en fonction de vos besoins -->
    <h3>Sender Information</h3>
    <p>Sender ID: {{ package.senderID }}</p>

    <h3>Package Reservations</h3>
    <ul>
      <li v-for="reservation in package.packageReservations" :key="reservation.id">
        <p>Reservation ID: {{ reservation.id }}</p>
        <p>Reservation Date: {{ reservation.reservationDate }}</p>
        <!-- Ajoutez d'autres champs de réservation en fonction de vos besoins -->
      </li>
    </ul>

    <h3>Addresses</h3>
    <ul>
      <li v-for="address in package.addresses" :key="address.id">
        <p>Address ID: {{ address.id }}</p>
        <p>Line 1: {{ address.line1 }}</p>
        <p>Line 2: {{ address.line2 }}</p>
        <!-- Ajoutez d'autres champs d'adresse en fonction de vos besoins -->
      </li>
    </ul>

    <h3>Documents</h3>
    <ul>
      <li v-for="document in package.documentS" :key="document.id">
        <p>Document ID: {{ document.id }}</p>
        <p>Document URL: {{ document.docURL }}</p>
        <!-- Ajoutez d'autres champs de document en fonction de vos besoins -->
      </li>
    </ul>

    <h3>Last Position</h3>
    <p>Last Position Latitude: {{ package.lastPositionLatitude }}</p>
    <p>Last Position Longitude: {{ package.lastPositionLongitude }}</p>
  </div>
</template>

<script>
export default {
  props: {
    package: Object, // Propriété pour recevoir les détails du colis
  },
};
</script>

<style scoped>
.package-details {
  padding: 20px;
  border: 1px solid #ccc;
  border-radius: 5px;
  margin: 10px;
}
</style>
