# vivalidi

[![Build Status](https://travis-ci.org/kubukoz/vivalidi.svg?branch=master)](https://travis-ci.org/kubukoz/vivalidi)
[![Maven Central](https://img.shields.io/maven-central/v/com.kubukoz/vivalidi_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cvivalidi)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Elegant, effect-agnostic validations for Scala DTOs

**Warning: this library is as experimental as they come. You're highly discouraged from using it in production**, although you might want to check it out in side projects or contribute to the upcoming planning for a more stable API.

## SBT dependency

```scala
"com.kubukoz" %% "vivalidi" % "x.x.x"
```

Replace x.x.x with the version you want (the latest non-snapshot release version can be seen on the top of this README).
The library is cross-compiled to Scala 2.12 and Scala.js - for the latter, replace `%%` with `%%%`.

## Usage
Coming soon - until then, please enjoy this sample:

```scala
type CreateV[T] = ValidatedNel[CreationError, T]

import com.kubukoz.vivalidi.Vivalidi._, dsl._

val result: F[CreateV[Transaction]] = Vivalidi.init[TransactionToCreate, CreateV, F]
  .pure(TransactionId(0))
  .sync(_.name)(validateLength(config.name.length))
  .sync(_.amount)(validateMoneyLimit(config.money.amount))
  .async(_.accounts)(checkSameAccount.liftF[F], validateAccounts(_).run(auth))
  .just(_.transferTime)
  .async(_.category)(sequencing(checkCategory(_)(auth)))
  .pure(auth)
  .to[Transaction]
  .run(toCreate)
```

tl;dr `CreateV[_] : Applicative` - so far the validators (e.g. `validateLength`, a custom function taking String and returning
`F[CreateV[String]]`) are responsible for raising errors, but in the future there might be no type parameter for errors, just
an appropriate `MonadError` context bound. `F[_]` is just an applicative (like `Future`), and `TransactionToCreate` is
the input type (which `toCreate` matches).

## FAQ

1. Can I use this in production?

![I mean you can, but you'll be on your own, as the API will change tens of times before it gets anywhere near stable](https://media.tenor.co/images/a8b0a72b4d23609c7f30b3ff2c3e9095/tenor.gif)

2. Does this have any external dependencies?

Yes, there's a direct dependency on `cats-core` and `shapeless`.
