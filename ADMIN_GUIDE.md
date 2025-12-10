# Admin Interface Setup Guide

## Database Migration

Before using the admin interface, you must run the database migration script:

```bash
./scripts/migrate_database.sh [path/to/database]
```

Default path is `data/mazerunner.db`. The script will:

1. Create a timestamped backup of your database
2. Add `user_type` column to the users table ('admin' or 'user')
3. Add `last_heartbeat` column to lobbies table for tracking inactive lobbies
4. Create `admin_metrics` table for system monitoring
5. Set the first registered user as admin if no admin exists
6. Clean up old metrics (older than 30 days)
7. Verify database integrity

### Setting Admin Users Manually

To promote a user to admin:

```bash
sqlite3 data/mazerunner.db "UPDATE users SET user_type = 'admin' WHERE username = 'YOUR_USERNAME';"
```

To demote an admin to regular user:

```bash
sqlite3 data/mazerunner.db "UPDATE users SET user_type = 'user' WHERE username = 'USERNAME';"
```

## Admin Dashboard Features

Access the admin dashboard at: `http://your-server/admin.html`

Only users with `user_type = 'admin'` can access this page.

### Real-Time System Monitoring

- **CPU Load**: System CPU load average
- **Memory Usage**: JVM heap memory usage (used/max and percentage)
- **Cache Hit Rate**: Percentage of cache hits vs misses
- **Active Requests**: Number of currently processing requests

System metrics are refreshed every 30 seconds automatically.

### Database Statistics

- Total users, games, bots, and mazes
- Database file size
- Table row counts

### Game Statistics

View game statistics for three time periods:
- **Daily**: Last 24 hours
- **Weekly**: Last 7 days
- **Monthly**: Last 30 days

Metrics include:
- Total games played
- Completed games
- Average score percentage
- Average steps taken
- Breakdown by difficulty (Easy/Medium/Hard)

### User Registration Stats

- New users registered in the last 7 days
- New users registered in the last 30 days
- Total registered users

### Average Wait Times

Shows average lobby wait times by difficulty level for the past 7 days, helping identify bottlenecks in matchmaking.

### Database Management Actions

- **Clean Old Metrics**: Remove metrics data older than 30 days
- **Cleanup Inactive Lobbies**: Remove lobbies inactive for more than 30 seconds
- **Vacuum Database**: Optimize database file (reclaim unused space)

### Metrics History Charts

View historical trends for:
- CPU Load
- Memory Usage %
- Cache Hit Rate
- Requests per Second

Time ranges: 1 hour, 6 hours, 24 hours, or 1 week

## Security

- Admin endpoints require authentication via JWT token
- All admin API calls verify `user_type = 'admin'`
- Non-admin users receive 403 Forbidden responses
- Session validation on every request

## API Endpoints

### GET /api/admin/dashboard
Returns comprehensive dashboard data including all metrics and statistics.

### GET /api/admin/system/status
Returns current real-time system metrics.

### GET /api/admin/metrics/history?type=<metric>&hours=<hours>
Returns historical metric data for charting.

Parameters:
- `type`: cpu_load, memory_usage_percent, cache_hit_rate, requests_per_second
- `hours`: Number of hours to look back (default: 24)

### POST /api/admin/database/manage
Performs database maintenance actions.

Request body:
```json
{
  "action": "cleanup_old_metrics|cleanup_inactive_lobbies|vacuum_database",
  "days": 30
}
```

## Metrics Collection

The `AdminMetricsService` automatically collects:

- System metrics every 30 seconds
- Request counts and response times
- Cache hit/miss rates
- Metrics are flushed to database every 60 seconds

Metrics are stored in the `admin_metrics` table with automatic cleanup of old data.

## Troubleshooting

### Admin page shows "Forbidden"
- Verify your user has `user_type = 'admin'`
- Check: `sqlite3 data/mazerunner.db "SELECT username, user_type FROM users;"`

### No data showing on charts
- Ensure the server has been running long enough to collect metrics
- Check that metrics collection service started (look for startup logs)
- Verify `admin_metrics` table has data

### Migration fails
- Check database file permissions
- Ensure SQLite3 is installed
- Review the backup file created before migration
- Check for any foreign key constraint violations

## Performance Considerations

- Metrics are collected in-memory and flushed periodically
- Chart rendering is done client-side using Canvas API
- Old metrics are automatically cleaned up to prevent database bloat
- Cache statistics help identify performance bottlenecks
