package gobblin.config.configstore.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;

import gobblin.config.configstore.ConfigStore;
import gobblin.config.configstore.ConfigStoreFactory;


public class ETLHdfsConfigStoreFactory implements ConfigStoreFactory<ConfigStore> {

  public static final String SCHEME_NAME = "etl-hdfs";
  public static final String CONFIG_STORE_NAME = "_CONFIG_STORE";
  private static final Logger LOG = Logger.getLogger(ETLHdfsConfigStoreFactory.class);

  @Override
  public String getScheme() {
    return SCHEME_NAME;
  }

  private String getActualScheme() {
    return "hdfs";
  }

  @Override
  public ETLHdfsConfigStore createConfigStore(URI uri) throws ConfigStoreCreationException, IOException {
    FileSystem fs = new Path(uri.getPath()).getFileSystem(new Configuration());
    if (!uri.getScheme().equals(this.getScheme())) {
      String errMesg =
          String.format("Input scheme %s does NOT match config store scheme %s", uri.getScheme(), this.getScheme());
      LOG.error(errMesg);
      throw new ConfigStoreCreationException(errMesg);
    }

    try {
      URI adjusted = new URI(getActualScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment());
      URI root = this.findConfigStoreRoot(adjusted, fs);
      return new ETLHdfsConfigStore(root);
    } catch (URISyntaxException e) {
      LOG.error("got error when constructing uri based on " + uri, e);
      throw new RuntimeException(e);
    }
  }

  private URI findConfigStoreRoot(URI input, FileSystem fs) throws ConfigStoreCreationException, IOException {
    Path p = new Path(input.getPath());

    
    while (p != null) {
      System.out.println("Check "+p);
      FileStatus[] fileStatus = fs.listStatus(p);
      for (FileStatus f : fileStatus) {
        if (!f.isDir() && f.getPath().getName().equals(CONFIG_STORE_NAME)) {
          String parent = f.getPath().getParent().toString();
          
          System.out.println("parent is " + parent);
          
          try {
            return new URI(parent);
          } catch (URISyntaxException e) {
            // Should not come here
            e.printStackTrace();
          }
        }
      }
      p = p.getParent();
    }

    throw new ConfigStoreCreationException("Can not find config store root in " + input);
  }

}