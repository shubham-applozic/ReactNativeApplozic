
Pod::Spec.new do |s|
  s.name         = "RNApplozicChat"
  s.version      = "1.0.0"
  s.summary      = "RNApplozicChat"
  s.description  = <<-DESC
                  RNApplozicChat
                   DESC
  s.homepage     = "https://www.applozic.com/"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "adarsh@applozic.com" }
  s.platform     = :ios, "10.0"
  s.source       = { :git => "https://github.com/AppLozic/Applozic-React-Native-Chat-Messaging-SDK.git", :tag => "master" }
  s.source_files  = "RNApplozicChat/**/*.{h,m}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency 'Applozic', '~> 8.3.0'
end
