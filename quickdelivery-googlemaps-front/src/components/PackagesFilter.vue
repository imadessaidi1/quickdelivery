<template>
    <div id="filter_section">
      <input type="text" v-model="searchQuery" placeholder="Recherche...">
      <div class="buttons_section">
        <button class="btn primary_btn">Search</button>
        <button class="btn danger_btn">Clear</button>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        searchQuery: '',
        items: [
          { id: 1, name: 'Item 1' },
          { id: 2, name: 'Item 2' },
          { id: 3, name: 'Another Item' },
          // Add more items as needed
        ]
      };
    },
    computed: {
      filteredItems() {
        // Filtrer les éléments en fonction de la recherche
        return this.items.filter(item =>
          item.name.toLowerCase().includes(this.searchQuery.toLowerCase())
        );
      }
    }
  }
  </script>
  
  <style scoped>
  #filter_section{
    width: 75%;
    margin: 15px auto;
  }
  #filter_section input{
    width: 50%;
    margin: 10px 0;
  }
  .buttons_section{
    width: 180px;
    display: flex;
    justify-content: space-between;
  }
  @media screen and (max-width: 600px) {
    #filter_section{
    width: 100%;
    }
    #filter_section input{
        width: 90%;
    }
  }
  </style>
  