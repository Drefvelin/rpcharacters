# Persistent parties

Parties created with `/rpcharacter party create <name>` persist across member and
leader logouts and server restarts. Party chat automatically finds online members
from the restored membership. Character switching does not change membership;
parties use player UUIDs.

Membership is saved after creation, joining, leaving, kicking, and disbanding in
`RPCharacters/data/parties.json`. Include this file in server backups. Saves use a
temporary file and replacement, and invalid saved data causes loading to fail
instead of silently resetting the parties.

Explicit `/rpcharacter party leave` still removes a member. If the leader leaves,
the party is disbanded, matching the existing command behaviour. Pending invites
remain temporary and are not restored on restart.

After upgrading from the in-memory implementation, players may need to create
and join their party once; there is no previous party file to import.

Verified by automated restart round trips, leader/member logout checks, persisted
leave/kick/disband checks, and invalid-file protection. Before live deployment,
create a party with two players, exchange party chat messages, disconnect both,
restart, and confirm chat still reaches both after reconnecting.
