output "instance_public_ip" {
  description = "IP publique de l'instance, à utiliser pour SSH et pour tester le site"
  value       = openstack_compute_instance_v2.app.access_ip_v4
}

output "instance_name" {
  description = "Nom de l'instance créée"
  value       = openstack_compute_instance_v2.app.name
}