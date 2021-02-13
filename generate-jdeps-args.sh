# todo dynamic version
jdeps --list-deps target/failchat-v2.6.4-SNAPSHOT/failchat-2.6.4-SNAPSHOT.jar | \
  sed 's/   //g' | \
  sed '/JDK removed internal API/d' | \
  sed '/java.base\//d' | \
  tr '\n' ',' | \
  sed 's/$/jdk.crypto.ec/' \
  > jdeps-args


