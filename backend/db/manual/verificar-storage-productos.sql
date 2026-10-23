-- Read-only policy verification. JWT claims are simulated inside this transaction only.
-- No objects, files, users, products, stock or roles are inserted/updated/deleted.
BEGIN;
DO $$
DECLARE p record; claims jsonb; test_bucket text; expression text; actual boolean; expected boolean;
BEGIN
  IF NOT (SELECT relrowsecurity FROM pg_class WHERE oid='storage.objects'::regclass) THEN
    RAISE EXCEPTION 'storage.objects must have RLS enabled';
  END IF;
  IF (SELECT count(*) FROM pg_policies WHERE schemaname='storage' AND tablename='objects' AND cmd IN ('INSERT','UPDATE','DELETE','ALL'))<>3 THEN
    RAISE EXCEPTION 'Unexpected additional Storage write policy';
  END IF;
  FOR p IN SELECT * FROM pg_policies WHERE schemaname='storage' AND tablename='objects' AND cmd IN ('INSERT','UPDATE','DELETE') LOOP
    IF p.roles <> ARRAY['authenticated']::name[] THEN RAISE EXCEPTION 'Unexpected policy roles: %',p.policyname; END IF;
    FOR claims IN SELECT value FROM jsonb_array_elements('[
      {"role":"anon"},
      {"role":"anon","app_metadata":{"role":"admin"}},
      {"role":"authenticated"},
      {"role":"authenticated","user_metadata":{"role":"admin"}},
      {"role":"authenticated","app_metadata":{"role":"customer"}},
      {"role":"authenticated","app_metadata":{"role":"admin"}}
    ]'::jsonb) LOOP
      PERFORM set_config('request.jwt.claims',claims::text,true);
      FOREACH test_bucket IN ARRAY ARRAY['productos','otro-bucket'] LOOP
        expected := COALESCE(claims->>'role'='authenticated' AND claims->'app_metadata'->>'role'='admin' AND test_bucket='productos',false);
        FOREACH expression IN ARRAY ARRAY[p.qual,p.with_check] LOOP
          IF expression IS NULL THEN CONTINUE; END IF;
          EXECUTE format('SELECT COALESCE((%s),false) FROM (SELECT $1::text AS bucket_id) object',expression) INTO actual USING test_bucket;
          actual := actual AND claims->>'role'='authenticated';
          IF actual IS DISTINCT FROM expected THEN RAISE EXCEPTION 'Policy matrix failed: %, bucket %, claims %',p.policyname,test_bucket,claims; END IF;
        END LOOP;
      END LOOP;
    END LOOP;
  END LOOP;
  IF NOT EXISTS(SELECT 1 FROM pg_policies WHERE schemaname='storage' AND tablename='objects' AND cmd='SELECT' AND policyname='Public Access Productos' AND roles=ARRAY['public']::name[]) THEN
    RAISE EXCEPTION 'Public catalog image read policy is missing';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM storage.buckets WHERE id='productos' AND public=true AND file_size_limit=8388608
    AND allowed_mime_types=ARRAY['image/jpeg','image/png','image/webp','image/avif']::text[]) THEN
    RAISE EXCEPTION 'Bucket image restrictions do not match';
  END IF;
END $$;
ROLLBACK;
SELECT 'OK' AS policy_matrix, 'No persistent data modified' AS result;
