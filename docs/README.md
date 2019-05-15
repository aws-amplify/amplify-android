# Documention setup

This README intends to provide instructions for setting up the documentation site locally for verifying the changes.

## Pre-requisites

To run the documentation website locally, we will need a set of tools which we need to install once, before we start previewing our changes:

1. **jekyll**

    ```ruby
    gem install jekyll
    ```

2. **bundler**

    ```ruby
    gem install bundler
    ```

## Previewing doc changes

1. Make changes you intend to make, e.g. in `index.md` file of `docs` folder.

2. Navigate the terminal to the `docs` folder.

3. Run `bundle install`

	> Note: This command is only required once to setup the dependencies for the doc website. Recommended to run again on fresh clones of repositories, but not when only updating and previewing.

4. Run `bundle exec jekyll serve --port 4001`

	> Note: You can use port number of your choice instead of 4001

## Updating API Reference

1. Run `./gradlew :aws-amplify-auth:generateReference`

	> Note: Run the command in the root directory.
