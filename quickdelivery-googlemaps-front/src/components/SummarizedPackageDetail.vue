<template>
    <!--<img :src="getImageSrc()" alt="Image" class="item-image" />-->
    <div class="item-details">
        <h3>{{ package_.id }}</h3>
        <!--<p>{{$t('packageHeight')}}:
            {{package_.height}}</p>
        <p>{{$t('packageWidth')}}:
            {{package_.width}} </p>
        <p>{{$t('packageDepth')}}:
            {{package_.dept}}</p>
        <p>{{$t('packageWeight')}}:
            {{package_.weight}}</p>-->
        <p>{{$t('packagePrice')}}:
            {{package_.price}}</p>
        <p>{{$t('packageDestination')}}:
            {{destinationAddress()}}</p>
        <p>{{$t('packageDistanceToDestination')}}:
            {{package_.distanceToDestination}}</p>
    </div>
</template>
<script>

export default{
    props: {
        package_: Object,
      },
    methods: {
       getImageSrc() {
          let imgSrc = '';
          if (this.package_.files && this.package_.files[0]) {
            imgSrc = `data:image/png;base64,${this.package_.files[0].data}`;
          }
          return imgSrc;
        },
        destinationAddress() {
          var destinationAddressS = '';
          this.package_.addresses.forEach(address => {
            if(address.type === 'ARRIVAL'){
              destinationAddressS = address.line1+" "+address.zipCode+" "+address.town+" "+address.country;
            }
          });
          return destinationAddressS;
        },
    }
}
</script>