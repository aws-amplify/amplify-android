{% include header.html %}
# AWS Amplify Storage

## Setup

Add gradle dependencies to begin using the library.
    
```groovy
// Required as a base for Storage
implementation 'com.amazonaws:aws-amplify-storage:{{ site.sdk_version }}'
```

## Authentication using static credentials

1. **Create the `Storage` client.**

    {% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

	 **Simple**
	 
	 The bucket name and AWS Region are read from `awsconfiguration.json` file. The `AWSCredentials` can be retrieved from the `Auth` object 	 constructed based on the `awsconfiguration.json` file.
	 
	 ```java
	 Storage storage = new S3Storage(getApplicationContext());
	 ```
	 
	 **Advanced**
	 
	 ```javaException
	 Storage storage = new S3Storage(getApplicationContext(), // Android Context
                "ap-southeast-1-bucket", // Bucket Name
                Regions.AP_SOUTHEAST_1, // AWS Region
                new Auth(getApplicationContext()), // Auth for AWS Credentials
                new StorageOptions().fileAccessLevel(FileAccessLevel.PRIVATE)); // Storage options to change file access level
	 ```

    {% include tab_content_end.html %}

## Operations

### Upload

The following example talks about how to upload a file.

1. **Upload a file with key (Sync)**

    {% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
    try {
    	storage.put("barcelona.jpeg, "/storage/emulated/0/barcelona.jpeg");
    } catch (Exception se) {
    	Log.e("Uploading failed. " + se);
    }
    ```

    {% include tab_content_end.html %}
    
2. **Upload a file with key and callback (Async)**

	 {% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
    storage.put("barcelona.jpeg", "/storage/emulated/0/barcelona.jpeg", new Callback<StorageResult>() {
            @Override
            public void onResult(StorageResult result) {
               
            }

            @Override
            public void onError(Exception se) {     
            	  Log.e("Uploading failed. " + ex);  
            }
        });
    ```
    
3. **Advanced: Resumability**

	{% include tabs.html %}

   {% include tab_content_start.html lang="java" %}

   ```java
   StorageTask storageTask = storage.put("barcelona.jpeg", "/storage/emulated/0/barcelona.jpeg", new Callback<StorageResult>() {
            @Override
            public void onResult(StorageResult result) {
               
            }

            @Override
            public void onError(Exception se) {     
            	  Log.e("Uploading failed. " + ex);  
            }
        });
        
   storageTask.pause();
   
   storageTask.resume();
   
   storageTask.cancel();
   ```
   
4. **Advanced: Monitor state and progress updates**

	{% include tabs.html %}

   {% include tab_content_start.html lang="java" %}

   ```java
   StorageTask storageTask = storage.put("barcelona.jpeg", "/storage/emulated/0/barcelona.jpeg", new Callback<StorageResult>() {
            @Override
            public void onResult(StorageResult result) {
               
            }

            @Override
            public void onError(Exception se) {     
            	  Log.e("Uploading failed. " + ex);  
            }
        });
        
    storageTask.setStorageStateCallback(new StorageStateCallback() {
       @Override
            public void onStateChanged(StorageState storageState) {
                Log.e("Storage", "onStateChanged: State = " + storageState);
                // updateUI(state);
            }
        });

    storageTask.setStorageProgressCallback(new StorageProgressCallback() {
        @Override
        public void onProgressChanged(StorageProgress storageProgress) {
            Log.e("Storage", "onProgressChanged: Progress = " + storageProgress.toString());
            // updateUI(storageProgress);
        }
    });
   ```
   
{% include tab_content_end.html %}

### Download

1. **Download a file with key (Sync)**

	 {% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
    try {
    	File myFile = storage.get("barcelona.jpeg", "/storage/emulated/0/barcelona.jpeg");
    } catch (Exception se) {
    	Log.e("Downloading failed. " + se);
    }
    ```
    
    {% include tab_content_end.html %}

2. **Download a file with key and callback (Async)**

	{% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
    storage.get("barcelona.jpeg", "/storage/emulated/0/barcelona.jpeg", new Callback<StorageResult>() {
            @Override
            public void onResult(StorageResult result) {
                File myFile = result.getFile();
            }

            @Override
            public void onError(Exception se) {
             		Log.e("Downloading failed. " + se);
            }
        });

    ```
    
3. **Advanced: Resumability**

	{% include tabs.html %}

   {% include tab_content_start.html lang="java" %}

   ```java
   StorageTask storageTask = storage.get("barcelona.jpeg", "/storage/emulated/0/barcelona.jpeg", new Callback<StorageResult>() {
            @Override
            public void onResult(StorageResult result) {
               
            }

            @Override
            public void onError(Exception se) {     
            	  Log.e("Uploading failed. " + ex);  
            }
        });
        
   storageTask.pause();
   
   storageTask.resume();
   
   storageTask.cancel();
   ```
   
4. **Advanced: Monitor state and progress updates**

	{% include tabs.html %}

   {% include tab_content_start.html lang="java" %}

   ```java
   StorageTask storageTask = storage.get("barcelona.jpeg", "/storage/emulated/0/barcelona.jpeg", new Callback<StorageResult>() {
            @Override
            public void onResult(StorageResult result) {
               
            }

            @Override
            public void onError(Exception se) {     
            	  Log.e("Uploading failed. " + ex);  
            }
        });
        
    storageTask.setStorageStateCallback(new StorageStateCallback() {
       @Override
            public void onStateChanged(StorageState storageState) {
                Log.e("Storage", "onStateChanged: State = " + storageState);
                // updateUI(state);
            }
        });

    storageTask.setStorageProgressCallback(new StorageProgressCallback() {
        @Override
        public void onProgressChanged(StorageProgress storageProgress) {
            Log.e("Storage", "onProgressChanged: Progress = " + storageProgress.toString());
            // updateUI(storageProgress);
        }
    });
   ```
    
{% include tab_content_end.html %}
    
### List

1. **List files under a prefix (Sync)**

	 {% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
     	 List<String> keys = null;

        try {
            // will get the files under public/videos since public is the default file access level
            keys = storage.list("videos");
        } catch (Exception se) {
            Log.e("Storage", "List failed with error." + se);
        }
    ```
    
    {% include tab_content_end.html %}

2. **List files under a prefix (Async)**

	{% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
    storage.list("videos", new Callback<List<String>>() {
            @Override
            public void onResult(List<String> keys) {
            		
            }

            @Override
            public void onError(Exception se) {
                Log.e("Storage", "List failed with error." + se);
            }
        });
    }
    ```
    
    {% include tab_content_end.html %}
    
### Remove

1. **Remove a file (Sync)**

	 {% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
        try {
            storage.remove("barcelona.jpeg");
        } catch (Exception se) {
            Log.e("Storage", "Remove failed with error." + se);
        }
    ```
    
    {% include tab_content_end.html %}

2. **Remove a file (Async)**

	{% include tabs.html %}

    {% include tab_content_start.html lang="java" %}

    ```java
    storage.remove("barcelona.jpeg", new Callback<Void>() {
            @Override
            public void onResult(Void result) {
            		
            }

            @Override
            public void onError(Exception se) {
                Log.e("Storage", "Remove failed with error." + se);
            }
        });
    }
    ```
    
    {% include tab_content_end.html %}
