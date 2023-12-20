{
  description = "FrostedHeart develop environment";

  inputs = {
    flake-utils.url = "github:numtide/flake-utils";
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  };

  outputs = { self, flake-utils, nixpkgs }:
    flake-utils.lib.eachDefaultSystem (system:
    let
      pkgs = nixpkgs.legacyPackages.${system};
    in {
      devShells.default = pkgs.mkShell (with pkgs; {
        packages = [
          jetbrains.idea-community
          (gradle_6.override {
            javaToolchains = [
              openjdk8
            ];
          })
        ];

        # Magically, this is the only native lib shipped by lwjgl
        # that needs patching.
        # The patched version, made avaliable as follows,
        # is further fed into `runClient`
        # by use of JVM parameter `-Dorg.lwjgl.glfw.libname`.
        LIBGLFW_PATH = glfw;
        MC_JAVA_HOME = openjdk8;
      });
    });
}
