import EventCard from "./EventCard";

function EventGrid({ events }) {
  if (!events.length) {
    return (
      <p className="empty-state">
        No hay eventos que coincidan con tu filtro actual.
      </p>
    );
  }

  return (
    <div className="event-grid">
      {events.map((event, index) => (
        <EventCard
          key={event.id}
          event={event}
          entranceDelay={Math.min(index % 9, 5) * 40}
        />
      ))}
    </div>
  );
}

export default EventGrid;
