package vivalidi

package object syntax {
  object all        extends SelectorSyntax with ValidatorSyntax
  object selectors  extends SelectorSyntax
  object validators extends ValidatorSyntax
}
