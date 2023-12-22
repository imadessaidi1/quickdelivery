<template>
  <div>
    <h2>Liste d'Adresses</h2>
    <div class="column-half">
      <div>
        <label>Prénom:</label>
        <input v-model="address.firstName" />
      </div>
      <div>
        <label>Nom:</label>
        <input type="text" v-model="address.lastName" />
      </div>
      <div>
        <label>Ligne 1:</label>
        <input v-model="address.line1" />
      </div>
      <div>
        <label>Ligne 2:</label>
        <input v-model="address.line2" />
      </div>
      </div>
      <div class="column-half">
      <div>
        <label>Ville:</label>
        <input v-model="address.town" />
      </div>
      <div>
        <label>Code Postal:</label>
        <input type="number" v-model="address.zipCode" />
      </div>
      <div>
        <label>Pays:</label>
        <input v-model="address.country" />
      </div>
      <div>
        <label>Type:</label>
        <select id="addressType" v-model="address.type">
          <option v-for="addressType in addressTypes" :key="addressType.key" :value="addressType.key">
            {{ addressType.value }}
          </option>
        </select>
      </div>
      </div>

    <button @click="addAddress">Ajouter une Adresse</button>
    <table>
      <thead>
        <tr>
          <th>Prénom</th>
          <th>Nom</th>
          <th>Ligne 1</th>
          <th>Ligne 2</th>
          <th>Ville</th>
          <th>Code Postal</th>
          <th>Pays</th>
          <th>Type</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(address, index) in addresses" :key="index">
          <td>{{ address.firstName }}</td>
          <td>{{ address.lastName }}</td>
          <td>{{ address.line1 }}</td>
          <td>{{ address.line2 }}</td>
          <td>{{ address.town }}</td>
          <td>{{ address.zipCode }}</td>
          <td>{{ address.country }}</td>
          <td>{{ address.type }}</td>
          <td>
            <button @click="editAddress(index)">Modifier</button>
            <button @click="removeAddress(index)">Supprimer</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  data() {
    return {
      addresses: [],
      address: {
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        type: "",
        latitude: 0,
        longitude: 0,
      },
      addressTypes: [
        {
        key: 'DEPARTURE',
        value: 'DEPARTURE'
        },
        {
        key: 'ARRIVAL',
        value: 'ARRIVAL'
        }
      ],
    };
  },
  methods: {
    addAddress() {
      this.addresses.push(this.address);
    },
    removeAddress(index) {
      this.addresses.splice(index, 1);
    },
    editAddress(index) {
      // Mettez en œuvre la logique pour éditer une adresse si nécessaire
      console.log("Modifier l'adresse à l'index", index);
    },
  },
};
</script>

<style scoped>
.address-item {
  border: 1px solid #ddd;
  padding: 10px;
  margin-bottom: 10px;
}
.column-half {
  width: 50%;
  box-sizing: border-box;
  float: left;
}
.column-half:nth-child(even) {
  clear: both;
}
label {
  display: block;
  margin-bottom: 5px;
}
</style>