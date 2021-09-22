# APKEY

## Overview

* Difficulty: Easy
* Category: Mobile

## Description
> This app contains some unique keys. Can you get one?

## Approach

1. The given file is an Android APK. We can disassemble it using JADX to take a look at it's source code.

2. Once disassembled, we can use `find` in the sources directory to list the whole tree structure.
We can see that the main java class has been disassembled into the file `com/example/apkey/MainActivity`.java. In this file we can see that this APK simply gets a string as input from the user, which then gets hashed using MD5 and then compares the result with a static hash. If the two hashes match, the app will display to the user the string generated from the functions `b.a(g.a())`.

```java
...
if (MainActivity.this.f928c.getText().toString().equals("admin")) {
    MainActivity mainActivity = MainActivity.this;
    b bVar = mainActivity.e;
    String obj = mainActivity.d.getText().toString();
    try {
        MessageDigest instance = MessageDigest.getInstance("MD5");
        instance.update(obj.getBytes());
        byte[] digest = instance.digest();
        StringBuffer stringBuffer = new StringBuffer();
        for (byte b2 : digest) {
            stringBuffer.append(Integer.toHexString(b2 & 255));
        }
        str = stringBuffer.toString();
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        str = "";
    }
    if (str.equals("a2a3d412e92d896134d9c9126d756f")) {
        Context applicationContext = MainActivity.this.getApplicationContext();
        MainActivity mainActivity2 = MainActivity.this;
        b bVar2 = mainActivity2.e;
        g gVar = mainActivity2.f;
        makeText = Toast.makeText(applicationContext, b.a(g.a()), 1);
        makeText.show();
    }
}
makeText = Toast.makeText(MainActivity.this.getApplicationContext(), "Wrong Credentials!", 0);
makeText.show();
...
```

3. As we can see from the imports of this class, these two functions get imported from the classes `b` and `g`, both contained in the package `c.b.a`. We can also find this package in our disassembled code:

```bash
[exyss@exyss sources]$ find . | grep c/b/a

./b/h/c/b/a.java
./c/b/a
./c/b/a/f.java
./c/b/a/b.java
./c/b/a/e.java
./c/b/a/i.java
./c/b/a/a.java
./c/b/a/h.java
./c/b/a/g.java
./c/b/a/c.java
./c/b/a/d.java
```

However, as we can see by opening the two java files, they both also import other packages contained in the two disassembled root packages (c and b)

4. We can create a Java project that uses these two root packages as dependecies and then run the two functions in MainActivity.java.

```bash
# Project Structure

[exyss@exyss eclipse-workspace]$ tree -d -L 4

solve_APKey
`-- src
    |-- b
    |   |-- a
    |   |-- b
    |   |-- c
    |   |-- d
    |   |-- e
    |   |-- f
    |   |-- g
    |   |-- h
    |   |-- i
    |   |-- j
    |   |-- k
    |   |-- l
    |   |-- m
    |   |-- n
    |   |-- o
    |   |-- p
    |   |-- q
    |   |-- r
    |   `-- s
    |-- c
    |   |-- a
    |   |-- b
    |   `-- c
    `-- solve_APKey
        `-- Solve.java
```

```java
// solve_APKey/src/solve_APKey/Solve.java

package solve_APKey;

import c.b.a.b;
import c.b.a.g;

public class Solve{

    public static void main(String[] args) {
    
        System.out.println(b.a(g.a()));
    }
}

```

5. After being compiled and run, the program will display the flag

## Flag

<details>
<summary>Click to view the flag</summary>

__HTB{m0r3_0bfusc4t1on_w0uld_n0t_hurt}__

</details>