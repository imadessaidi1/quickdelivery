<template>
  <div>
    <h2>Liste d'Adresses</h2>
    <div class="column-half">
      <div>
        <label>{{$t('packageAddressFirstName')}}:</label>
        <input v-model="address.firstName" required="true" />
      </div>
      <div>
        <label>{{$t('packageAddressLastName')}}:</label>
        <input type="text" v-model="address.lastName" required="true" />
      </div>
      <div>
        <label>{{$t('packageAddressLine1')}}:</label>
        <input v-model="address.line1" required="true" />
      </div>
      <div>
        <label>{{$t('packageAddressLine2')}}:</label>
        <input v-model="address.line2" />
      </div>
      </div>
      <div class="column-half">
      <div>
        <label>{{$t('packageAddressCity')}}:</label>
        <input v-model="address.town" />
      </div>
      <div>
        <label>{{$t('packageAddressZip')}}:</label>
        <input type="number" v-model="address.zipCode" oninput="javascript: if (this.value.length > this.maxLength) this.value = this.value.slice(0, this.maxLength);" maxlength="5" required="true"/>
      </div>
      <div>
        <label>{{$t('packageAddressCountry')}}:</label>
        <input v-model="address.country" required="true" />
      </div>
      <div>
        <label>{{$t('packageAddressType')}}:</label>
        <select id="addressType" v-model="address.type">
          <option v-for="addressType in addressTypes" :key="addressType.key" :value="addressType.key">
            {{ addressType.value }}
          </option>
        </select>
      </div>
      </div>

    <button @click="addAddress" :disabled="addresses.length === 2">{{$t('packageAddressAddAction')}}</button>
    <table>
      <thead>
        <tr>
          <th>{{$t('packageAddressFirstName')}}</th>
          <th>{{$t('packageAddressLastName')}}</th>
          <th>{{$t('packageAddressLine1')}}</th>
          <th>{{$t('packageAddressLine2')}}</th>
          <th>{{$t('packageAddressCity')}}</th>
          <th>{{$t('packageAddressZip')}}</th>
          <th>{{$t('packageAddressCountry')}}</th>
          <th>{{$t('packageAddressType')}}</th>
          <th>{{$t('packageAddressListActions')}}</th>
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
            <button @click="removeAddress(index)">{{$t('packageAddressListActionsDelete')}}</button>
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
      if(this.addresses.length < 2){
      const typeExists = this.addresses.some(address => address.type === this.address.type);
      if (!typeExists) {
        this.addresses.push({ ...this.address});
      }else{
        alert('Type d\'adresse existant');
      }
      }else{
      alert('Vous ne pouvez pas ajouter plus de 2 adresses');
      }
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
.invalid-field {
  border: 1px solid red;
}
</style>