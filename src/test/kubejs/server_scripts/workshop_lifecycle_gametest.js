FirstworksEvents.workshopProcessingStarting(event => {
  if (String(event.recipeId) === 'firstworks:gametest_kubejs_cancel') {
    // Encode one listener invocation into observable workshop state without
    // loading arbitrary Java classes through Rhino's sandbox.
    event.workshop.stoke(event.workshop.getStokeTicks() + 20)
    event.cancel()
  }
})

FirstworksEvents.workshopProcessingCompleted(event => {
  if (String(event.recipeId) === 'firstworks:gametest_kubejs_complete') {
    event.workshop.stoke(event.workshop.getStokeTicks() + 37)
  }
})
