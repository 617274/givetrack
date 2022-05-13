# Givetrack

An app for searching and saving charities, then posting, organizing and visualizing records from charitable contributions (monetary consideration, in-kind goods, volunteer service).

Webpage: https://coded.art/givetrack

## Releases

Both variants are enrolled in beta testing.
* Free: https://play.google.com/store/apps/details?id=art.coded.givetrack.free
* Paid: https://play.google.com/store/apps/details?id=art.coded.givetrack.paid

## Attribution

This app is made with:

* [CharityNavigator](http://api.charitynavigator.org/) for charity data
* [Firebase](https://firebase.google.com) for user authentication and database
* [AdMob](https://admob.google.com) for reward generation
* [Stripe](https://stripe.com) for payment processing (in-development)
* [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for charting
* [Butterknife](https://github.com/JakeWharton/butterknife) for dependency injection
* [Timber](https://github.com/JakeWharton/timber) for logging
* [Glide](https://github.com/bumptech/glide) for image caching
* [Jsoup](https://jsoup.org) for HTML parsing

## Prerequisites

1. Register with [Google Play Services](https://developer.android.com/distribute/play-services/) to generate an API key and config file.
2. Register with [Google AdMob](https://developers.google.com/ads/) to generate an app and rewarded ad ID.
3. Register with [Charity Navigator](http://api.charitynavigator.org/) to generate an app ID and key.
4. Add the above generated IDs, keys and files to your project without exposing them in a public repository.