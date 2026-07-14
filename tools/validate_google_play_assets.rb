#!/usr/bin/env ruby
# frozen_string_literal: true

require "pathname"

ROOT = Pathname.new(__dir__).join("..").expand_path
PLAY_ROOT = ROOT.join("assets", "store-listing", "google-play")
PNG_SIGNATURE = "\x89PNG\r\n\x1a\n".b

def fail_asset(message)
  warn(message)
  exit(1)
end

def png_dimensions(path)
  header = path.binread(24)
  fail_asset("Not a PNG file: #{path}") unless header.start_with?(PNG_SIGNATURE)
  fail_asset("Invalid PNG header: #{path}") unless header.byteslice(12, 4) == "IHDR"

  header.byteslice(16, 8).unpack("NN")
end

def require_dimensions(path, expected_width, expected_height)
  fail_asset("Missing asset: #{path}") unless path.file?
  actual = png_dimensions(path)
  expected = [expected_width, expected_height]
  fail_asset("Expected #{path} to be #{expected.join('x')}, got #{actual.join('x')}") unless actual == expected
end

def validate_screenshot_set(relative_dir, expected_width, expected_height)
  paths = PLAY_ROOT.join(relative_dir).glob("*.png").sort
  fail_asset("No PNG screenshots found in #{relative_dir}") if paths.empty?

  paths.each do |path|
    require_dimensions(path, expected_width, expected_height)
    width, height = png_dimensions(path)
    short_edge, long_edge = [width, height].sort
    fail_asset("Play screenshot edge out of range for #{path}") unless short_edge >= 320 && long_edge <= 3840
    fail_asset("Play screenshot aspect ratio exceeds 2:1 for #{path}") if long_edge.to_f / short_edge > 2.0
  end

  paths.length
end

require_dimensions(PLAY_ROOT.join("icon-512x512.png"), 512, 512)
require_dimensions(PLAY_ROOT.join("feature-graphic-1024x500.png"), 1024, 500)

phone_count = validate_screenshot_set("screenshots", 1080, 1920)
seven_inch_count = validate_screenshot_set("seven-inch-tablet-screenshots", 1200, 1920)
ten_inch_count = validate_screenshot_set("ten-inch-tablet-screenshots", 2560, 1600)

text_limits = {
  "listing/en-US/title.txt" => 30,
  "listing/en-US/short_description.txt" => 80,
  "listing/en-US/full_description.txt" => 4000,
}
text_limits.each do |relative_path, limit|
  path = PLAY_ROOT.join(relative_path)
  fail_asset("Missing listing text: #{path}") unless path.file?
  length = path.read(encoding: "UTF-8").strip.length
  fail_asset("#{relative_path} is #{length} characters; limit is #{limit}") if length > limit
end

puts "Google Play assets valid: 1 icon, 1 feature graphic, " \
     "#{phone_count} phone, #{seven_inch_count} seven-inch, " \
     "#{ten_inch_count} ten-inch screenshots"
