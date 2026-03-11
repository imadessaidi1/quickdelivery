const { spawnSync } = require("child_process");
const path = require("path");

const projectRoot = path.resolve(__dirname, "..");
const iosProjectPath = path.join(projectRoot, "ios");

if (process.platform !== "darwin") {
  console.error(
    [
      "Impossible d'ouvrir le projet iOS sur cet environnement.",
      `Projet natif disponible: ${iosProjectPath}`,
      "L'ouverture de l'app iOS nécessite macOS + Xcode + CocoaPods.",
      "Utilise `npm run cap:sync:ios` ici, puis `npm run cap:open:ios` sur un Mac."
    ].join("\n")
  );
  process.exit(1);
}

const result = spawnSync("npx", ["cap", "open", "ios"], {
  cwd: projectRoot,
  stdio: "inherit",
  shell: true
});

process.exit(result.status ?? 1);
