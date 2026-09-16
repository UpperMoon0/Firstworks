const JavaSystem = Java.loadClass('java.lang.System')

function incrementProperty(key) {
  const current = Number(JavaSystem.getProperty(key, '0'))
  JavaSystem.setProperty(key, String(current + 1))
}

FirstworksEvents.workshopProcessingStarting(event => {
  if (String(event.recipeId) === 'firstworks:gametest_kubejs_cancel') {
    incrementProperty('firstworks.gametest.workshopStartCount')
    event.cancel()
  }
})

FirstworksEvents.workshopProcessingCompleted(event => {
  if (String(event.recipeId) === 'firstworks:gametest_kubejs_complete') {
    incrementProperty('firstworks.gametest.workshopCompleteCount')
  }
})
