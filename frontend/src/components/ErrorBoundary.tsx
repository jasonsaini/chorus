import { Component, type ErrorInfo, type ReactNode } from "react";

type Props = { children: ReactNode };

type State = { hasError: boolean; message: string };

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: "" };

  static getDerivedStateFromError(err: Error): State {
    return { hasError: true, message: err.message };
  }

  componentDidCatch(err: Error, info: ErrorInfo): void {
    console.error(err, info.componentStack);
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div
          style={{
            minHeight: "100vh",
            padding: "2rem",
            background: "#0c0e12",
            color: "#e8eaef",
            fontFamily: "system-ui, sans-serif",
          }}
        >
          <h1 style={{ marginTop: 0 }}>Chorus couldn’t start</h1>
          <p style={{ color: "#9aa3b2" }}>
            Check the browser console (Developer Tools → Console) for details.
          </p>
          <pre
            style={{
              padding: "1rem",
              borderRadius: 8,
              background: "#12151c",
              overflow: "auto",
              fontSize: "0.875rem",
            }}
          >
            {this.state.message}
          </pre>
        </div>
      );
    }
    return this.props.children;
  }
}
