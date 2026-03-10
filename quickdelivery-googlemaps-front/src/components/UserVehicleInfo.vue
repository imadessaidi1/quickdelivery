<template>
    <div class="vehicle-form">
        <h2>{{ $t('userVehicleInfoTitle') }}</h2>
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
                <option value="ELECTRIC">{{ $t('ELECTRIC') }}</option>
                <option value="HYBRID">{{ $t('HYBRID') }}</option>
                <option value="METHANE">{{ $t('METHANE') }}</option>
                <option value="ETHANOL">{{ $t('ETHANOL') }}</option>
                <option value="GASOLINE">{{ $t('GASOLINE') }}</option>
                <option value="DIESEL">{{ $t('DIESEL') }}</option>
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
                <br/><span v-if="vehicleDocuments && vehicleDocuments['GRAY_CARD'] != undefined"><strong>{{vehicleDocuments['GRAY_CARD'].name}}</strong>
                <span v-if="vehicleDocuments['GRAY_CARD'].documentStatus === 'REJECTED'">❌</span>
                <span v-if="vehicleDocuments['GRAY_CARD'].documentStatus === 'ACCEPTED'">✅</span></span>
                <span v-if="filesErrorMessages['GRAY_CARD']" class="errorMessage" >{{filesErrorMessages['GRAY_CARD']}}</span>
            </div>
            <div class="input_only">
                <label for="INSURANCE">{{$t('userVehicleInsurance')}} :</label>
                <input  ref="fileInput1"
                        :id="INSURANCE"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleVehicleFileChange(1, 'INSURANCE')"
                />
                <br/><span v-if="vehicleDocuments && vehicleDocuments['INSURANCE'] != undefined"><strong>{{vehicleDocuments['INSURANCE'].name}}</strong>
                <span v-if="vehicleDocuments['INSURANCE'].documentStatus === 'REJECTED'">❌</span>
                <span v-if="vehicleDocuments['INSURANCE'].documentStatus === 'ACCEPTED'">✅</span></span>
                <span v-if="filesErrorMessages['INSURANCE']" class="errorMessage" >{{filesErrorMessages['INSURANCE']}}</span>
            </div>
        </div>
    </div>
</template>

<script>
import { Field, ErrorMessage } from 'vee-validate';
import { validateCarRegistrationNumber, validateRequired } from '@/config/comonFunction';
import { ref } from 'vue';
export default {
  components: {
    Field,
    ErrorMessage,
  },
  props: {
    isForUpdate: ref(false),
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
      INSURANCE: 'INSURANCE',
      filesErrorMessages: [],
    };
  },
  methods: {
    validateCarRegistrationNumber,
    validateRequired,
    handleVehicleFileChange(index, type) {
      const fileInput = this.$refs[`fileInput${index}`];
      const file_ = fileInput.files[0];
      if (file_) {
        if(this.isForUpdate){
            this.vehicleDocuments[type] = {file: file_, name: file_.name, documentStatus: 'UPDATED'};
        }else{
            this.vehicleDocuments[type] = {file: file_, name: file_.name, documentStatus: 'ACCEPTED'};
        }
      }
    },
  },
};
</script>
