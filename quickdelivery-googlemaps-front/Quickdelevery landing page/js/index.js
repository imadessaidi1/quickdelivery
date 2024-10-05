function commencer() {
  alert("Commençons votre livraison !");
}
// navbar transition
window.addEventListener('scroll', function() {
  var navbar = document.getElementById('navbar');
  if (window.scrollY > 200) { // Lorsque le scroll dépasse 100px
    navbar.classList.add('scrolled'); // Ajoute la classe 'scrolled'
  } else {
    navbar.classList.remove('scrolled'); // Retire la classe si on est au-dessus
  }
});