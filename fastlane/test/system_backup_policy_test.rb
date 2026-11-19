# frozen_string_literal: true

require "minitest/autorun"

class SystemBackupPolicyTest < Minitest::Test
  ROOT = File.expand_path("../..", __dir__)

  def test_android_manifest_keeps_system_backup_enabled
    manifest = read("androidApp/src/main/AndroidManifest.xml")

    assert_includes manifest, 'android:allowBackup="true"'
    assert_includes manifest, 'android:fullBackupContent="@xml/backup_rules"'
    assert_includes manifest, 'android:dataExtractionRules="@xml/data_extraction_rules"'
  end

  def test_android_backup_rules_include_database_and_preferences
    [
      "androidApp/src/main/res/xml/backup_rules.xml",
      "androidApp/src/main/res/xml/data_extraction_rules.xml"
    ].each do |path|
      rules = read(path)

      assert_includes rules, '<include domain="database" path="." />', path
      assert_includes rules, '<include domain="sharedpref" path="." />', path
      refute_includes rules, "<exclude", path
    end
  end

  def test_android_extraction_rules_cover_cloud_backup_and_device_transfer
    rules = read("androidApp/src/main/res/xml/data_extraction_rules.xml")

    assert_includes rules, "<cloud-backup>"
    assert_includes rules, "<device-transfer>"
  end

  def test_ios_sources_do_not_exclude_app_data_from_system_backups
    sources = Dir.glob(File.join(ROOT, "shared/src/iosMain/**/*.kt")).sort

    refute_empty sources

    sources.each do |path|
      content = File.read(path)

      refute_includes content, "NSURLIsExcludedFromBackupKey", path
      refute_includes content, "isExcludedFromBackup", path
    end
  end

  def test_ios_database_uses_the_backed_up_default_container
    factory = read("shared/src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/IosPlatformAdapters.kt")

    assert_includes factory, 'NativeSqliteDriver(WorkoutDatabase.Schema, "oops_all_prs.db")'
    refute_includes factory, "NSDocumentDirectory", "the database must not be relocated to a hand-managed directory"
  end

  private

  def read(relative_path)
    File.read(File.join(ROOT, relative_path))
  end
end
