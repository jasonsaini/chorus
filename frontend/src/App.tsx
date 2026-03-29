import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { HomePage } from "./pages/HomePage";

const RoomPage = lazy(async () => {
  const m = await import("./pages/RoomPage");
  return { default: m.RoomPage };
});

function RoomFallback() {
  return (
    <div className="route-loading">
      <p>Loading room…</p>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route
        path="/room/:roomId"
        element={
          <Suspense fallback={<RoomFallback />}>
            <RoomPage />
          </Suspense>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
