<template>
    <div class="vehicle-form">
        <h2>Vehicle info.</h2>
    <div class="input_container">
        <div class="input_only">
            <label for="registrationNumber">Registration number:</label>
            <Field type="text" id="registrationNumber" v-model="user.vehicle.registrationNumber" name="registrationNumber" :rules="validateCarRegistrationNumber" />
            <ErrorMessage class="errorMessage" name="registrationNumber" />
        </div>
        <div class="input_only">
            <label for="brand">Brand:</label>
            <Field type="text" id="brand" v-model="user.vehicle.brand" name="brand" :rules="validateRequired" />
            <ErrorMessage class="errorMessage" name="brand" />
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="model">Model:</label>
            <Field type="text" id="model" v-model="user.vehicle.model" name="model" :rules="validateRequired" />
            <ErrorMessage class="errorMessage" name="model" />
        </div>
        <div class="input_only">
            <label for="energyType">Energy Type:</label>
            <select id="energyType" v-model="user.vehicle.energyType">
                <option value="ELECTRIC">Electric</option>
                <option value="HYBRID">Hybrid</option>
                <option value="METHANE">Methane</option>
                <option value="ETHANOL">Ethanol</option>
                <option value="GASOLINE">Gasoline</option>
                <option value="DIESEL">Diesel</option>
            </select>
        </div>
    </div>
        <div class="picture_file_container">
            <div class="input_only">
                <label for="GRAY_CARD">Gray card :</label>
                <input  ref="fileInput0"
                        :id="GRAY_CARD"
                        type="file"
                        accept="image/*"
                @change="handleVehicleFileChange(0,'GRAY_CARD')"
                />
                <br/><span v-if="user.vehicle.vehicleDocuments[0] != undefined"><strong>{{user.vehicle.vehicleDocuments[0].file.name}}</strong></span>
            </div>
            <div class="input_only">
                <label for="INSURANCE">Insurance :</label>
                <input  ref="fileInput1"
                        :id="INSURANCE"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleVehicleFileChange(1, 'INSURANCE')"
                />
                <br/><span v-if="user.vehicle.vehicleDocuments[1] != undefined"><strong>{{user.vehicle.vehicleDocuments[1].file.name}}</strong></span>
            </div>
        </div>
    </div>
</template>

<script>
import { Field, ErrorMessage } from 'vee-validate';
import { validateCarRegistrationNumber, validateRequired } from '@/config/comonFunction';
export default {
  components: {
    Field,
    ErrorMessage,
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
  },
  methods: {
    validateCarRegistrationNumber,
    validateRequired,
    handleVehicleFileChange(index, type) {
      const fileInput = this.$refs[`fileInput${index}`];
      const file_ = fileInput.files[0];
      if (file_) {
        let vehicleDocument = {
            type: type,
            file: file_,
        };
        this.user.vehicle.vehicleDocuments[index] = vehicleDocument;
      }
    },
  },
};
</script>