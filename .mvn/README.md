These JVM flags are required by ErrorProne (the javac plugin enabled on the core module):
it needs the internal javac packages opened to the unnamed module.
Do not remove them unless ErrorProne is dropped from the build.
Note: .mvn/jvm.config does not support comments, so this explanation lives in a separate file.
