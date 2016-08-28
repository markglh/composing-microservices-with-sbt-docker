
CREATE KEYSPACE IF NOT EXISTS beacon_service WITH REPLICATION = {
  'class': 'NetworkTopologyStrategy',
  'DATA' : 2
};

CREATE TABLE IF NOT EXISTS beacon_service.beacon_by_location (
    location_id uuid,
    beacon_id uuid,
    PRIMARY KEY (location_id)
);

