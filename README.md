CheckValve 2.0
==============

CheckValve 2.0 is currently under development.  This version of CheckValve includes many changes:

**Bug fixes**
- Added better exception handling
- Fixed a few NPEs.

**Code Changes**
- Completely changed the handling of database queries
  - `DatabaseProvider` instances are now opened and closed more cleanly within each class that uses it
  - Cursors are only used within methods of the DatabaseProvider class
  - SQLite queries are now wrapped in `synchronized()` blocks
- Made compatible with API 11+ (Android Honeycomb and above)
  - Moved all network operations to background threads
- Redesigned UI screens for easier navigation
- Removed unused code, classes, layouts, and string entires.

**New Features**
- Settings
  - General
    - Show RCON passwords
    - Validate new servers
  - RCON
    - Show suggested commands (auto-fill)
    - Warn before sending unsafe commands
  - Server Information
    - Show server name
    - Show IP/Port
    - Show game and version
    - Show number of players
    - Show map name
    - Show server tags
  - Default query options
    - Default port
    - Default timeout
  - Default Chat Relay server
    - IP
    - Port
    - Password
- View chat
  - Requires CheckValve Chat Relay

