<!-- PackageForm.vue -->

<template>
  <div class="package-form">
    <h2>{{$t('createNewPackage')}}</h2>
    <form @submit.prevent="submitForm" ref="packageCreationForm">
    <table>
    <tr>
    <td>
    <div class="form-group">
        <label for="height">{{$t('packageHeight')}}:</label>
        <input type="number" v-model="package_.height" required />

      </div>
    </td>
    <td>
      <div class="form-group">
        <label for="width">{{$t('packageWidth')}}:</label>
        <input type="number" v-model="package_.width" required />
      </div>
    </td>
    </tr>
    <tr>
      <td>
      <div class="form-group">
        <label for="depth">{{$t('packageDepth')}}:*</label>
        <input type="number" v-model="package_.depth" required />
      </div>
      </td>
      <td>
      <div class="form-group">
        <label for="weight">{{$t('packageWeight')}}:</label>
        <input type="number" v-model="package_.weight" required />
      </div>
      </td>
    </tr>
    <tr>
      <td>
      <div class="form-group">
        <label for="pictureURL">{{$t('packagePicture')}}:</label>
        <input type="text" v-model="package_.pictureURL" />
      </div>
      </td>
      <td></td>
    </tr>
    </table>
      <PackageAddress :addresses="package_.addresses"/>
      <button type="submit">Create Package</button>
    </form>
  </div>
</template>

<script>
import PackageAddress from './PackageAddress.vue';
export default {
  components: {
    PackageAddress,
  },
  data() {
    return {
      package_: {
        height: 0,
        width: 0,
        weight: 0,
        depth: 0,
        pictureURL: '',
        addresses: [
                    {
                      firstName: '',
                      lastName: '',
                      line1: '',
                      line2: '',
                      town: '',
                      zipCode: '',
                      country: '',
                      type: ''
                    }
                  ],
        documentS: [
          {
            docURL: '',
            type: ''
          }
        ],
      },
    };
  },
  methods: {
    submitForm() {
      // Envoyez le package à l'API REST ici
      console.log('Package to be sent:', this.package);
      // Ajoutez la logique pour envoyer les données à l'API REST
    },
  },
};
</script>

<style scoped>
.package-form {
  padding: 20px;
  border: 1px solid #ccc;
  border-radius: 5px;
  margin: 10px;
}

.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
}
.column-half {
  width: 50%;
  box-sizing: border-box;
  float: left;
}
.column-half:nth-child(even) {
  clear: both;
}

</style>
