<template>
  <div>
    <h1>Offres Airbnb</h1>
    <div v-for="listing in listings" :key="listing.id" class="listing">
      <img :src="listing.image" alt="Listing Image">
      <div>
        <h2>{{ listing.title }}</h2>
        <p>{{ listing.description }}</p>
        <p>{{ listing.price }} € / nuit</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      listings: [
        {
          id: 1,
          title: 'Appartement moderne au centre-ville',
          description: 'Magnifique appartement avec vue imprenable',
          price: 120,
          image: 'https://example.com/apartment1.jpg',
        },
        // Ajoutez plus d'annonces selon vos besoins
      ],
    };
  },
};
</script>

<style>
.listing {
  display: flex;
  border: 1px solid #ccc;
  margin: 10px;
  padding: 10px;
}

.listing img {
  width: 150px;
  height: 150px;
  margin-right: 10px;
}
</style>
