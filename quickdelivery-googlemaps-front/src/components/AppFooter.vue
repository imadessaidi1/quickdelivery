<!-- Footer.vue -->

<template>
  <div>
    <footer>
      <div class="footer-column">
        <h4>Assistance</h4>
        <ul>
          <li>Centre d'aide</li>
          <li>Lutte contre la discrimination</li>
          <li>Assistance handicap</li>
        </ul>
      </div>
      <div class="footer-column">
        <h4>Accueil</h4>
        <ul>

          <li>Forum de la communauté</li>
        </ul>
      </div>
      <div class="footer-column">
        <h4>QuickDelivery</h4>
        <ul>
          <li>A propos de nous</li>
          <li>Nous contacter</li>
        </ul>
      </div>
    </footer>
    <div class="copyright-info">
      <div class="left-align">
        <p>© 2023 QuickDelivery, Inc.·Confidentialité ·Conditions générales ·Plan du site ·Fonctionnement du sit ·Infos sur l'entreprise</p>
      </div>
      <div class="right-align">
        <p>Français (FR)&nbsp;€ EUR</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Ajoutez du style pour le pied de page */
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

.horizontal-line {
  border-top: 1px solid #ccc;
  margin-top: 20px;
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
