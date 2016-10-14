# Documentation: https://github.com/Homebrew/brew/blob/master/docs/Formula-Cookbook.md
#                http://www.rubydoc.info/github/Homebrew/brew/master/Formula
# PLEASE REMOVE ALL GENERATED COMMENTS BEFORE SUBMITTING YOUR PULL REQUEST!

class FabAndroidScripts < Formula
  desc ""
  homepage ""
  url "https://github.com/thefabulous/fab-android-scripts/archive/master.tar.gz"
  sha256 ""

  # depends_on "cmake" => :build
  depends_on :x11 # if your formula requires any X11/XQuartz components

  def install
    bin.install 'src/devtools.groovy' => 'devtools'
    bin.install 'src/adbwifi.groovy' => 'adbwifi'
    bin.install 'src/adbscreenrecord.groovy' => 'adbscreenrecord'

    # ENV.deparallelize  # if your formula fails when building in parallel

    # Remove unrecognized options if warned by configure
    system "./configure", "--disable-debug",
                          "--disable-dependency-tracking",
                          "--disable-silent-rules",
                          "--prefix=#{prefix}"
    # system "cmake", ".", *std_cmake_args
    system "make", "install" # if this fails, try separate make/make install steps
  end
end
