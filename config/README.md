# config

Configuration source for the Spring Cloud Config Server.

This repository uses the local `config/` folder from the same project as the native config source.
The Config Server resolves it from:

- `file:${user.dir}/config` when launched from the project root
- `file:${user.dir}/../config` when launched from the `quickdemivery-config-server` module

In Docker deployment, the same folder is mounted into the container and exposed through
`SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS`.
