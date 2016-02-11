package object models {
  type Attempt[T] = Either[AMIableErrors, T]
}
