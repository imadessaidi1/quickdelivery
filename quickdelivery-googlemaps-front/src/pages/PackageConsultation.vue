<template>
    <div class="summary_component">
        <h2>{{$t('packageSummaryAction')}}</h2>
        <PackageSummary/>
    </div>
    <br/>
    <button class="primary_btn" ref="detailsButtons"
    @click="reserve">{{ $t('packagesArroundMArkerDetailActionsReserve') }}</button>
</template>
<script>
import PackageSummary from '../components/PackageDetails.vue';
import http from '@/config/httpInterceptor';

export default{
  components: {
        PackageSummary,
  },
  data() {
    return {
      package: {
        id: null,
        version: null,
        creationDate: null,
        reference: "",
        height: 0,
        width: 0,
        depth: 0,
        weight: 0,
        pictureURL: "",
        status: "",
        deliveryPrice: null,
        senderID: null,
        packageReservations: [],
        addresses: [{
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "DEPARTURE",
        latitude: 0,
        longitude: 0,
      },
      {
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "ARRIVAL",
        latitude: 0,
        longitude: 0,
      }],
      lastPositionLatitude: null,
      lastPositionLongitude: null
    }
    };
  },
  mounted() {
    const id = this.$route.params.id;
    http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage')+id)
      .then(response => {
        this.$store.commit('updatePackage', response.data);
    }).catch(() => {
      console.log("unable to process your request this time. please try again latter.");
    });
  },
  methods: {
    reserve() {
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.$store.state.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id;
      return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$store.commit('updatePackage', this.package);
            this.$store.commit('updateDocuments', []);
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.log("unable to process your request this time. please try again latter.");
        });
    },
  },
}
</script>