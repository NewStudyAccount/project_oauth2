## ADDED Requirements

### Requirement: Infrastructure config directory
项目 SHALL 包含 `infra/` 目录，集中管理基础设施相关配置文件。

#### Scenario: Infra directory exists
- **WHEN** 查看项目根目录
- **THEN** 存在 `infra/` 目录

### Requirement: Hosts config in infra
`hosts.txt` SHALL 位于 `infra/` 目录下，而非根目录。

#### Scenario: Hosts file relocated
- **WHEN** 查看 `infra/` 目录
- **THEN** 存在 `hosts.txt` 文件

#### Scenario: Hosts file removed from root
- **WHEN** 查看项目根目录
- **THEN** 不存在 `hosts.txt` 文件

### Requirement: Docker compose in infra
`infra/` 目录 SHALL 包含 `docker-compose.yml` 用于一键启动 MySQL、Redis、Nacos 等基础设施。

#### Scenario: Docker compose file exists
- **WHEN** 查看项目 `infra/` 目录
- **THEN** 存在 `docker-compose.yml` 文件，定义 MySQL、Redis、Nacos 服务