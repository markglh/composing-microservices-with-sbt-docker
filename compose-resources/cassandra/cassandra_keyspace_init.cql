
-- Each service holds it's data in an exclusive keyspace

CREATE KEYSPACE IF NOT EXISTS beacon_service WITH REPLICATION = {
  'class': 'NetworkTopologyStrategy',
  'DATA' : 2
};

CREATE TABLE IF NOT EXISTS beacon_service.beacons_by_location (
    location_id uuid,
    beacon_id uuid,
    beacon_name text,
    PRIMARY KEY (location_id)
);

CREATE KEYSPACE IF NOT EXISTS tracking_service WITH REPLICATION = {
  'class': 'NetworkTopologyStrategy',
  'DATA' : 2
};

CREATE TABLE IF NOT EXISTS tracking_service.users_by_beacon (
    beacon_id uuid,
    time_logged timestamp,
    user_id uuid,
    name text,
    PRIMARY KEY (beacon_id, time_logged, user_id)
) WITH CLUSTERING ORDER BY (time_logged DESC);


-- Init the data for beacons_by_location
insert into beacon_service.beacons_by_location (location_id, beacon_id, beacon_name ) VALUES ( 6bcb1c95-a283-468a-a7ee-ce7f21168b71, 06ea8543-c6f2-4b32-96e2-89a88338403b, 'Room1 Beacon');

insert into beacon_service.beacons_by_location (location_id, beacon_id, beacon_name ) VALUES ( 54acaf0a-bd29-4b40-b36e-bc63314ab5b4, 3f60d4de-99d3-4652-bb42-db69ea0d2e6d, 'Room2 Beacon');

insert into beacon_service.beacons_by_location (location_id, beacon_id, beacon_name ) VALUES ( 804ece81-86e0-4887-abb0-41b8aec08dd8, 250cd22b-1de4-4a6c-9ee5-761bf39204d0, 'Room3 Beacon');

insert into beacon_service.beacons_by_location (location_id, beacon_id, beacon_name ) VALUES ( ea47b13b-dc37-4523-830b-922bbb0dabc3, 42c75451-e0da-4481-b807-580bbb8c4bf2, 'Room4 Beacon');

insert into beacon_service.beacons_by_location (location_id, beacon_id, beacon_name ) VALUES ( 0b748d95-ad9e-4bd3-89ce-1f5fab50e81e, 44700c71-8d3b-42f8-a9d0-e5d2064d355b, 'Room5 Beacon');


-- Init the data for users_by_beacon (All same time, equivalent to ms epoch time: 1473156000)
insert into tracking_service.users_by_beacon (beacon_id, time_logged, user_id, name) VALUES ( 06ea8543-c6f2-4b32-96e2-89a88338403b, '2016-09-06 10:00:00+0000', f525cff6-721e-11e6-8b77-86f30ca893d3 ,'Chris Goldbrook');

insert into tracking_service.users_by_beacon (beacon_id, time_logged, user_id, name) VALUES ( 06ea8543-c6f2-4b32-96e2-89a88338403b, '2016-09-06 10:00:00+0000', 8010fa28-721f-11e6-8b77-86f30ca893d3 ,'Andy Stevens');

insert into tracking_service.users_by_beacon (beacon_id, time_logged, user_id, name) VALUES ( 06ea8543-c6f2-4b32-96e2-89a88338403b, '2016-09-06 10:00:00+0000', 88b66e10-721f-11e6-8b77-86f30ca893d3 ,'Nev Best');

insert into tracking_service.users_by_beacon (beacon_id, time_logged, user_id, name) VALUES ( 3f60d4de-99d3-4652-bb42-db69ea0d2e6d, '2016-09-06 10:00:00+0000', 94e11b68-721f-11e6-8b77-86f30ca893d3 ,'Tony Freeds');

insert into tracking_service.users_by_beacon (beacon_id, time_logged, user_id, name) VALUES ( 3f60d4de-99d3-4652-bb42-db69ea0d2e6d, '2016-09-06 10:00:00+0000', 99970b22-721f-11e6-8b77-86f30ca893d3 ,'Olly Maverick');

insert into tracking_service.users_by_beacon (beacon_id, time_logged, user_id, name) VALUES ( 3f60d4de-99d3-4652-bb42-db69ea0d2e6d, '2016-09-06 10:00:00+0000', 9ebc0c38-721f-11e6-8b77-86f30ca893d3 ,'Nat Portside');

insert into tracking_service.users_by_beacon (beacon_id, time_logged, user_id, name) VALUES ( 44700c71-8d3b-42f8-a9d0-e5d2064d355b, '2016-09-06 10:00:00+0000', a4619f9a-721f-11e6-8b77-86f30ca893d3 ,'Adam Sundown');
