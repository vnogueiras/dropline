
-- ============================================================
-- Dropline Catalog Schema
-- Version: V1
-- ============================================================

-- Venues
CREATE TABLE venue (
                       id UUID PRIMARY KEY,
                       name VARCHAR(120) NOT NULL,
                       city VARCHAR(80) NOT NULL,
                       timezone VARCHAR(40) NOT NULL,
                       layout JSONB NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Events
CREATE TABLE event (
                       id UUID PRIMARY KEY,
                       venue_id UUID NOT NULL REFERENCES venue(id),
                       title VARCHAR(200),
                       description TEXT,
                       image_url TEXT,
                       starts_at TIMESTAMPTZ,
                       ends_at TIMESTAMPTZ,
                       sale_starts_at TIMESTAMPTZ,
                       status VARCHAR(20)
                           CHECK (
                               status IN (
                                          'DRAFT',
                                          'PUBLISHED',
                                          'ON_SALE',
                                          'SOLD_OUT',
                                          'FINISHED'
                                   )
                               ),
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ
);

-- Price tiers
CREATE TABLE price_tier (
                            id UUID PRIMARY KEY,
                            event_id UUID NOT NULL REFERENCES event(id),
                            name VARCHAR(40),
                            price_cents INT CHECK (price_cents >= 0),
                            currency CHAR(3) DEFAULT 'EUR'
);

-- Seats
CREATE TABLE seat (
                      id UUID PRIMARY KEY,
                      event_id UUID NOT NULL REFERENCES event(id),
                      section_path VARCHAR(200),
                      row_label VARCHAR(4),
                      seat_number INT,
                      tier_id UUID REFERENCES price_tier(id),

                      CONSTRAINT uq_seat_event_location
                          UNIQUE (event_id, section_path, row_label, seat_number)
);

-- Indexes
CREATE INDEX idx_event_status_starts_at
    ON event(status, starts_at);

CREATE INDEX idx_seat_event_id
    ON seat(event_id);
