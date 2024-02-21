<template>
    <div class="vehicle-form">
        <h2>Add Vehicle</h2>
    <div class="input_container">
        <div class="input_only">
            <label for="brand">Registration number:</label>
            <input type="text" id="brand" v-model="vehicle.brand" />
        </div>
        <div class="input_only">
            <label for="brand">Brand:</label>
            <input type="text" id="brand" v-model="vehicle.brand" />
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="model">Model:</label>
            <input type="text" id="model" v-model="vehicle.model" />
        </div>
        <div class="input_only">
            <label for="energyType">Energy Type:</label>
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
                <label for="pictureFile">Gray card :</label>
                <input  ref="fileInput0"
                        :id="pictureFile"
                        type="file"
                        accept="image/*"
                @change="handleFileChange(0)"
                />
            </div>
            <div class="input_only">
                <label for="documentFile">Insurance :</label>
                <input  ref="fileInput1"
                        :id="documentFile"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleFileChange(1)"
                />
            </div>
        </div>
    </div>
</template>

<script>
export default {
  data() {
    return {
      vehicle: {
        type: Object,
        required: true,
      },
      newDocumentURL: '',
      newDocumentType: 'VEHICLE_GRY_CARD',
    };
  },
};
</script>