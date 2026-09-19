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

FirstworksEvents.mortarGrindingStarting(event => {
  if (String(event.recipeId).startsWith('firstworks:gametest_mortar_')) {
    const data = event.mortar.getPersistentData()
    data.putInt('starts', data.getInt('starts') + 1)
    if (String(event.recipeId) === 'firstworks:gametest_mortar_cancel') event.cancel()
  }
})
FirstworksEvents.mortarGrindingCompleted(event => {
  if (String(event.recipeId).startsWith('firstworks:gametest_mortar_')) {
    const data = event.mortar.getPersistentData()
    data.putInt('completions', data.getInt('completions') + 1)
  }
})

ServerEvents.recipes(event => {
  event.custom({
    type: 'firstworks:workshop_processing', station: 'stone_anvil',
    ingredient: { item: 'minecraft:blaze_powder' }, result: { id: 'minecraft:blaze_rod' },
    forge: { actions: ['draw', 'bend'], heat_ticks: 0 }
  }).id('firstworks:gametest_kubejs_forge')
  event.custom({
    type: 'firstworks:mortar_grinding', ingredient: { item: 'minecraft:sugar' },
    result: { id: 'minecraft:paper' }, processing: [{ action: 'crush', count: 1 }]
  }).id('firstworks:gametest_kubejs_stages')
})
