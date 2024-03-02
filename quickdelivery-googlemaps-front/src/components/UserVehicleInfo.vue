<template>
    <div class="vehicle-form">
        <h2>Vehicle info.</h2>
    <div class="input_container">
        <div class="input_only">
            <label for="registrationNumber">{{$t('userVehicleRegistration')}}:</label>
            <Field type="text" id="registrationNumber" v-model="vehicle.registrationNumber" name="registrationNumber" :rules="validateCarRegistrationNumber" />
            <ErrorMessage class="errorMessage" name="registrationNumber" />
        </div>
        <div class="input_only">
            <label for="brand">{{$t('userVehicleBrand')}}:</label>
            <Field type="text" id="brand" v-model="vehicle.brand" name="brand" :rules="validateRequired" />
            <ErrorMessage class="errorMessage" name="brand" />
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="model">{{$t('userVehicleModel')}}:</label>
            <Field type="text" id="model" v-model="vehicle.model" name="model" :rules="validateRequired" />
            <ErrorMessage class="errorMessage" name="model" />
        </div>
        <div class="input_only">
            <label for="energyType">{{$t('userVehicleEnergy')}}:</label>
            <select id="energyType" v-model="vehicle.energyType">
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
                <label for="GRAY_CARD">{{$t('userVehicleGryCard')}} :</label>
                <input  ref="fileInput0"
                        :id="GRAY_CARD"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleVehicleFileChange(0,'GRAY_CARD')"
                />
                <br/><span v-if="vehicleDocuments['GRAY_CARD'] != undefined"><strong>{{vehicleDocuments['GRAY_CARD'].name}}</strong></span>
            </div>
            <div class="input_only">
                <label for="INSURANCE">{{$t('userVehicleInsurance')}} :</label>
                <input  ref="fileInput1"
                        :id="INSURANCE"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleVehicleFileChange(1, 'INSURANCE')"
                />
                <br/><span v-if="vehicleDocuments['INSURANCE'] != undefined"><strong>{{vehicleDocuments['INSURANCE'].name}}</strong></span>
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
    vehicle() {
      return this.$store.state.vehicle;
    },
    vehicleDocuments() {
      return this.$store.state.vehicleDocuments;
    },
  },
  data() {
    return {
      GRAY_CARD: 'GRAY_CARD',
      INSURANCE: 'INSURANCE'
    };
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
        console.log(vehicleDocument);
        this.vehicleDocuments[type] = file_;
      }
    },
  },
};
</script>