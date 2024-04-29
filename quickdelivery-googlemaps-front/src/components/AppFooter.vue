<template>
  <div class="footer">
    <!--<footer>
      <div class="footer-column">
        <h4>{{$t('footerAssistance')}}</h4>
        <ul>
          <li><a href="#">{{$t('footerAssistanceHelp')}}</a></li>
        </ul>
      </div>
      <div class="footer-column">
        <h4>{{$t('footerWelcome')}}</h4>
        <ul>
          <li>{{$t('footerWelcomeForum')}}</li>
        </ul>
      </div>
      <div class="footer-column">
        <h4>{{$t('applicationName')}}</h4>
        <ul>
          <li>{{$t('footerWelcomeAbout')}}</li>
          <li>{{$t('footerWelcomeContact')}}</li>
        </ul>
      </div>
    </footer>-->
    <div class="copyright-info">
      <p>© 2023 {{$t('applicationName')}}, Inc.·{{$t('footerPrivacy')}} ·{{$t('footerConditions')}} ·{{$t('footerCompany')}}</p>
    </div>
  </div>
</template>

<style scoped>
.footer {
  min-height: 50px;
}
.footer-column {
  flex: 1;
  margin-right: 20px;
}

.footer-column h4 {
  font-size: 18px;
  margin-bottom: 10px;
}

.footer-column ul {
  list-style: none;
  padding: 0;
}

.copyright-info {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #DBE2EF;
  padding: 0 10px;
}

.copyright-info p {
  flex: 1;
  text-align: center;
  font-size: 12px;
  margin: 0;
}
</style>
