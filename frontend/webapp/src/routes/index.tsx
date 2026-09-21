
import { createFileRoute } from "@tanstack/react-router";
import { StreamViewer } from "@/components/StreamViewer"; // camelCase file

// If you want a thin wrapper, use createEventSource from your service:
// import { createEventSource } from "@/domain/stream/services/eventSourceClient";

export const Route = createFileRoute("/")({
  component: () => {
    //const connect = useEventStreamStore((s) => s.connect);


    return <StreamViewer />;
  }
});
