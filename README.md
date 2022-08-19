# Markdown Widget

Add a minimalistic widget with the content of a markdown file to your home screen.
This app is intended for users of [Obsidian](https://obsidian.md), but the widget will display any markdown file on your phone.

If you like this app, consider buying me a coffee.

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/Tiim)

## Features

* Open any markdown or text file on your phone and display it as an home screen widget.
* The widget is updated in a regular interval.
* Supports all standard markdown features and some extensions:
    * Task Lists
    * Markdown Tables
    * Strikethrough
    * WikiLinks
    * Frontmatter

Currently the widget is non-interactive. Tapping on links or scrolling the widget does not do anything.

## Planned Features

* LaTeX support
* [Admonitions / Callouts](https://help.obsidian.md/How+to/Use+callouts)
* Highlighting

## Development
### New Release:

- Update version and version code in [build.gradle](app/build.gradle).
- Commit file `git add . && git commit`
- Add tag `git tag vX.Y.Z`
- Push to github `git push && git push --tags`


## License

[GNU General Public License v3.0](https://github.com/Tiim/Android-Markdown-Widget/blob/main/LICENSE.md)


