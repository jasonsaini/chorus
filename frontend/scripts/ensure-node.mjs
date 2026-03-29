const major = Number(process.versions.node.split(".")[0]);
if (Number.isNaN(major) || major < 18) {
  console.error(
    `[chorus] This app needs Node.js 18 or newer (Vite 5). You are on ${process.version}.`,
  );
  console.error(
    "[chorus] Fix: install Node 20 LTS, or run: cd frontend && nvm install && nvm use",
  );
  process.exit(1);
}
