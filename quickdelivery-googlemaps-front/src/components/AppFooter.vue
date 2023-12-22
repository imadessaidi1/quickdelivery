<template>
  <div>
    <footer>
      <div class="footer-column">
        <h4>{{$t('footerAssistance')}}</h4>
        <ul>
          <li>{{$t('footerAssistanceHelp')}}</li>
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
    </footer>
    <div class="copyright-info">
      <div class="left-align">
        <p>© 2023 {{$t('applicationName')}}, Inc.·{{$t('footerPrivacy')}} ·{{$t('footerConditions')}} ·{{$t('footerCompany')}}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
footer {
  display: flex;
  justify-content: space-around;
  background-color: #f0f0f0;
  padding: 20px;
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
  display: flex;
  justify-content: space-between;
  padding: 20px;
  background-color: #f0f0f0;
}

.left-align {
  flex: 1;
}

.right-align {
  flex: 1;
  text-align: right;
}
</style>
