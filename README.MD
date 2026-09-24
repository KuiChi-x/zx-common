# zx-common

Reusable Java 21 and Spring Boot 3 common libraries. The modules contain no company-specific service, registry, credential, or internal network dependency.

## Modules

- `common-base`: JSON, reflection, date, HTTP, AOP, retry, and conversion helpers
- `common-lock`: Redis based distributed lock aspect
- `common-rpc`: annotation driven HTTP clients
- `common-crawl`: Jsoup and HtmlUnit crawling with annotated field mapping
- `common-web`: response wrapping and exception handling
- `common-repository`: Spring Data JPA query and repository extensions

## Requirements

- JDK 21
- Maven 3.9+
- Spring Boot 3.5+

Build all modules:

```bash
mvn -DskipTests verify
```

## GitHub 自动发布到 Maven Central

### 一次性配置

1. 在 [Sonatype Central Portal](https://central.sonatype.com/) 注册并验证邮箱。
2. 在 Portal 中申请并验证 `cn.supzx.utils` namespace。该 namespace 必须能证明你拥有对应域名；如果无法验证，需要把 `groupId` 改成你能证明所有权的坐标，例如 `io.github.<GitHub用户名>`。
3. 在 Portal 的账号设置中生成 User Token，保存 token username 和 token password。
4. 创建 GPG 签名密钥，并把公钥上传到公共 key server：

   ```bash
   gpg --full-generate-key
   gpg --list-secret-keys --keyid-format LONG
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

   使用 `pub` 行的主密钥指纹（`[SC]`），不要使用下面 `sub` 行的加密子密钥（`[E]`）。完整指纹或末尾 16 位长 ID 都可以，例如：

   ```text
   6DB196230AA10A3189A14ADA6673D3403BBC56CC
   ```

   导出 ASCII 私钥内容，稍后粘贴到 GitHub Secret：

   ```bash
   gpg --armor --export-secret-keys <KEY_ID>
   ```

   Windows 可以安装 Gpg4win；在 PowerShell 或 Git Bash 中执行上述命令。
   在 PowerShell 中也可以直接复制私钥到剪贴板：

   ```powershell
   gpg --armor --export-secret-keys <KEY_ID> | Set-Clipboard
   ```

   只把公钥上传到 key server；私钥只粘贴到 GitHub Secret，不要上传到 key server 或提交到 Git。

5. 打开 GitHub 仓库的 **Settings → Secrets and variables → Actions → New repository secret**，添加：

   | Secret | 内容 |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | Sonatype Central User Token username |
   | `MAVEN_CENTRAL_TOKEN` | Sonatype Central User Token password |
   | `MAVEN_GPG_PRIVATE_KEY` | `-----BEGIN PGP PRIVATE KEY BLOCK-----` 到结束的完整内容 |
   | `MAVEN_GPG_PASSPHRASE` | GPG 私钥密码 |

   私钥和密码不要提交到代码仓库，也不要写入 `settings.xml`。

6. 确认 `.github/workflows/build.yml` 和 `.github/workflows/release.yml` 已经提交到默认分支，并在 **Settings → Actions → General** 中允许 GitHub Actions 运行。

### 每次发布

流水线不会自动改版本号；版本由你在 IDE 中手工维护。发布 `2.0.1` 时：

1. 在所有 `pom.xml` 中把项目当前版本 `2.0.0` 改成 `2.0.1`：包括根 POM、子模块 parent 版本、显式子模块版本和 `common-base` 依赖版本。不要修改 Spring Boot、Lombok 等第三方依赖版本。
2. 提交版本变更并创建同名 tag：

   ```bash
   git add pom.xml common-*/pom.xml
   git commit -m "chore: release 2.0.1"
   git tag v2.0.1
   git push origin master v2.0.1
   ```

   如果默认分支是 `main`，把命令里的 `master` 换成 `main`。
3. tag 推送后，`.github/workflows/release.yml` 自动运行。它会检查 tag 和所有 POM 版本一致，然后使用 JDK 21 构建、GPG 签名并发布父 POM 和六个模块。
4. 在 GitHub 仓库的 **Actions → Release to Maven Central** 查看结果。发布成功后，Central Portal 通常需要几分钟同步到公共搜索服务。

版本不一致、namespace 未验证、GPG 公钥未同步或 Secrets 缺失时，流水线会停止，不会发布半套产物。已发布的同一版本不能覆盖，修复后请递增版本号重新发布。
